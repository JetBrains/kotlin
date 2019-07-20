/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.fir

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.io.PrintStream
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.system.measureNanoTime


fun checkFirProvidersConsistency(firFiles: List<FirFile>) {
    for ((session, files) in firFiles.groupBy { it.fileSession }) {
        val provider = session.service<FirProvider>() as FirProviderImpl
        provider.ensureConsistent(files)
    }
}

private data class FailureInfo(val transformer: KClass<*>, val throwable: Throwable, val file: String)
private data class ErrorTypeReport(val report: String, var count: Int = 0)

class FirResolveBench(val withProgress: Boolean) {

    val timePerTransformer = mutableMapOf<KClass<*>, Long>()
    val counterPerTransformer = mutableMapOf<KClass<*>, Long>()
    var resolvedTypes = 0
    var errorTypes = 0
    var unresolvedTypes = 0
    var errorFunctionCallTypes = 0
    var errorQualifiedAccessTypes = 0
    var implicitTypes = 0
    var fileCount = 0


    private val fails = mutableListOf<FailureInfo>()
    val hasFiles get() = fails.isNotEmpty()

    private val errorTypesReports = mutableMapOf<String, ErrorTypeReport>()

    fun countBuilder(builder: RawFirBuilder, time: Long) {
        timePerTransformer.merge(builder::class, time) { a, b -> a + b }
        counterPerTransformer.merge(builder::class, 1) { a, b -> a + b }
    }

    fun processFiles(
        firFiles: List<FirFile>,
        transformers: List<FirTransformer<Nothing?>>
    ) {
        fileCount += firFiles.size
        try {
            for ((stage, transformer) in transformers.withIndex()) {
                println("Starting stage #$stage. $transformer")
                val firFileSequence = if (withProgress) firFiles.progress("   ~ ") else firFiles.asSequence()
                for (firFile in firFileSequence) {
                    var fail = false
                    val time = measureNanoTime {
                        try {
                            transformer.transformFile(firFile, null)
                        } catch (e: Throwable) {
                            val ktFile = firFile.psi as KtFile
                            println("Fail in file: ${ktFile.virtualFilePath}")
                            fail = true
                            fails += FailureInfo(transformer::class, e, ktFile.virtualFilePath)
                            //println(ktFile.text)
                            //throw e
                        }
                    }
                    if (!fail) {
                        timePerTransformer.merge(transformer::class, time) { a, b -> a + b }
                        counterPerTransformer.merge(transformer::class, 1) { a, b -> a + b }
                    }
                    //totalLength += StringBuilder().apply { FirRenderer(this).visitFile(firFile) }.length
                }
                checkFirProvidersConsistency(firFiles)
            }

            if (fails.none()) {
                println("SUCCESS!")
            } else {
                println("ERROR!")
            }
        } finally {


            val fileDocumentManager = FileDocumentManager.getInstance()

            firFiles.forEach {
                it.accept(object : FirVisitorVoid() {

                    fun reportProblem(problem: String, psi: PsiElement) {
                        val document = try {
                            fileDocumentManager.getDocument(psi.containingFile.virtualFile)
                        } catch (t: Throwable) {
                            throw Exception("for file ${psi.containingFile}", t)
                        }
                        val line = (document?.getLineNumber(psi.startOffset) ?: 0)
                        val char = psi.startOffset - (document?.getLineStartOffset(line) ?: 0)
                        val report = "e: ${psi.containingFile?.virtualFile?.path}: (${line + 1}:$char): $problem"
                        errorTypesReports.getOrPut(problem) { ErrorTypeReport(report) }.count++
                    }

                    override fun visitElement(element: FirElement) {
                        element.acceptChildren(this)
                    }

                    override fun visitFunctionCall(functionCall: FirFunctionCall) {
                        val typeRef = functionCall.typeRef
                        if (typeRef is FirResolvedTypeRef) {
                            val type = typeRef.type
                            if (type is ConeKotlinErrorType) {
                                errorFunctionCallTypes++
                            }
                        }

                        super.visitFunctionCall(functionCall)
                    }

                    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
                        val typeRef = qualifiedAccessExpression.typeRef
                        if (typeRef is FirResolvedTypeRef) {
                            val type = typeRef.type
                            if (type is ConeKotlinErrorType) {
                                errorQualifiedAccessTypes++
                            }
                        }

                        super.visitQualifiedAccessExpression(qualifiedAccessExpression)
                    }

                    override fun visitTypeRef(typeRef: FirTypeRef) {
                        unresolvedTypes++

                        if (typeRef.psi != null) {
                            val psi = typeRef.psi!!
                            val problem = "${typeRef::class.simpleName}: ${typeRef.render()}"
                            reportProblem(problem, psi)
                        }
                    }

                    override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef) {
                        if (implicitTypeRef is FirResolvedTypeRef) {
                            visitResolvedTypeRef(implicitTypeRef)
                        } else {
                            visitTypeRef(implicitTypeRef)
                        }
                    }

                    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
                        resolvedTypes++
                        val type = resolvedTypeRef.type
                        if (type is ConeKotlinErrorType || type is ConeClassErrorType) {
                            if (resolvedTypeRef.psi == null) {
                                implicitTypes++
                            } else {
                                errorTypes++
                                val psi = resolvedTypeRef.psi!!
                                val problem = "${resolvedTypeRef::class.simpleName} -> ${type::class.simpleName}: ${type.render()}"
                                reportProblem(problem, psi)
                            }
                        }
                    }
                })
            }
        }


    }

    fun throwFailure() {
        if (fails.any()) {
            val (transformerClass, failure, file) = fails.first()
            throw AssertionError("Failures detected in ${transformerClass.simpleName}, file: $file", failure)
        }
    }

    fun report(stream: PrintStream, errorTypeReports: Boolean = true) {

        if (errorTypeReports)
            errorTypesReports.values.sortedByDescending { it.count }.forEach {
                stream.print("${it.count}:")
                stream.println(it.report)
            }

        infix fun Int.percentOf(other: Int): String {
            return String.format("%.1f%%", this * 100.0 / other)
        }

        val totalTypes = unresolvedTypes + resolvedTypes
        stream.println("UNRESOLVED (UNTOUCHED) IMPLICIT TYPES: $unresolvedTypes (${unresolvedTypes percentOf totalTypes})")
        stream.println("RESOLVED TYPES: $resolvedTypes (${resolvedTypes percentOf totalTypes})")
        val goodTypes = resolvedTypes - errorTypes - implicitTypes
        stream.println("CORRECTLY RESOLVED TYPES: $goodTypes (${goodTypes percentOf resolvedTypes} of resolved)")
        stream.println("ERRONEOUSLY RESOLVED TYPES: $errorTypes (${errorTypes percentOf resolvedTypes} of resolved)")
        stream.println("   - unresolved calls: $errorFunctionCallTypes")
        stream.println("   - unresolved q.accesses: $errorQualifiedAccessTypes")
        stream.println("ERRONEOUSLY RESOLVED IMPLICIT TYPES: $implicitTypes (${implicitTypes percentOf resolvedTypes} of resolved)")
        stream.println("UNIQUE ERROR TYPES: ${errorTypesReports.size}")


        var totalTime = 0L
        var totalFiles = 0L

        timePerTransformer.forEach { (transformer, time) ->
            val counter = counterPerTransformer[transformer]!!
            stream.println("${transformer.simpleName}, TIME: ${time * 1e-6} ms, TIME PER FILE: ${(time / counter) * 1e-6} ms, FILES: OK/E/T $counter/${fileCount - counter}/$fileCount")
            totalTime += time
            totalFiles += counter
        }

        if (counterPerTransformer.keys.size > 0) {
            totalFiles /= counterPerTransformer.keys.size
            stream.println("Total, TIME: ${totalTime * 1e-6} ms, TIME PER FILE: ${(totalTime / totalFiles) * 1e-6} ms")
        }
    }
}

fun doFirResolveTestBench(
    firFiles: List<FirFile>,
    transformers: List<FirTransformer<Nothing?>>,
    gc: Boolean = true,
    withProgress: Boolean = false
) {

    if (gc) {
        System.gc()
    }

    val bench = FirResolveBench(withProgress)
    bench.processFiles(firFiles, transformers)
    bench.report(System.out)
    bench.throwFailure()
}


fun <T> Collection<T>.progress(label: String, step: Double = 0.1): Sequence<T> {
    return progress(step) { label }
}

fun <T> Collection<T>.progress(step: Double = 0.1, computeLabel: (T) -> String): Sequence<T> {
    val intStep = max(1, (this.size * step).toInt())
    var progress = 0
    val startTime = System.currentTimeMillis()

    fun Long.formatTime(): String {
        return when {
            this < 1000 -> "${this}ms"
            this < 60 * 1000 -> "${this / 1000}s ${this % 1000}ms"
            else -> "${this / (60 * 1000)}m ${this % (60 * 1000) / 1000}s ${this % (60 * 1000) % 1000}ms"
        }
    }

    return asSequence().onEach {
        if (progress % intStep == 0) {
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - startTime

            val eta = if (progress > 0) ((elapsed / progress * 1.0) * (this.size - progress)).toLong().formatTime() else "Unknown"
            println("${computeLabel(it)}: ${progress * 100 / size}% ($progress/${this.size}), ETA: $eta, Elapsed: ${elapsed.formatTime()}")
        }
        progress++
    }
}