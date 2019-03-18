/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.fir

import com.intellij.openapi.fileEditor.FileDocumentManager
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.system.measureNanoTime

fun doFirResolveTestBench(
    firFiles: List<FirFile>,
    transformers: List<FirTransformer<Nothing?>>,
    gc: Boolean = true,
    withProgress: Boolean = false
) {

    if (gc) {
        System.gc()
    }

    val timePerTransformer = mutableMapOf<KClass<*>, Long>()
    val counterPerTransformer = mutableMapOf<KClass<*>, Long>()
    val totalLength = 0
    var resolvedTypes = 0
    var errorTypes = 0
    var unresolvedTypes = 0


    try {
        for ((stage, transformer) in transformers.withIndex()) {
            println("Starting stage #$stage. $transformer")
            val firFileSequence = if (withProgress) firFiles.progress("   ~ ") else firFiles.asSequence()
            for (firFile in firFileSequence) {
                val time = measureNanoTime {
                    try {
                        transformer.transformFile(firFile, null)
                    } catch (e: Throwable) {
                        val ktFile = firFile.psi as KtFile
                        println("Fail in file: ${ktFile.virtualFilePath}")
                        println(ktFile.text)
                        throw e
                    }
                }
                timePerTransformer.merge(transformer::class, time) { a, b -> a + b }
                counterPerTransformer.merge(transformer::class, 1) { a, b -> a + b }
                //totalLength += StringBuilder().apply { FirRenderer(this).visitFile(firFile) }.length
            }
        }


        println("SUCCESS!")
    } finally {

        var implicitTypes = 0


        val errorTypesReports = mutableMapOf<String, String>()

        val fileDocumentManager = FileDocumentManager.getInstance()

        firFiles.forEach {
            it.accept(object : FirVisitorVoid() {
                override fun visitElement(element: FirElement) {
                    element.acceptChildren(this)
                }

                override fun visitTypeRef(typeRef: FirTypeRef) {
                    unresolvedTypes++
                }

                override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
                    resolvedTypes++
                    val type = resolvedTypeRef.type
                    if (type is ConeKotlinErrorType || type is ConeClassErrorType) {
                        if (resolvedTypeRef.psi == null) {
                            implicitTypes++
                        } else {
                            val psi = resolvedTypeRef.psi!!
                            val problem = "$type with psi `${psi.text}`"
                            val document = try {
                                fileDocumentManager.getDocument(psi.containingFile.virtualFile)
                            } catch (t: Throwable) {
                                throw Exception("for file ${psi.containingFile}", t)
                            }
                            val line = document?.getLineNumber(psi.startOffset) ?: 0
                            val char = psi.startOffset - (document?.getLineStartOffset(line) ?: 0)
                            val report = "e: ${psi.containingFile?.virtualFile?.path}: ($line:$char): $problem"
                            errorTypesReports[problem] = report
                            errorTypes++
                        }
                    }
                }
            })
        }

        errorTypesReports.forEach {
            println(it.value)
        }


        println("TOTAL LENGTH: $totalLength")
        println("UNRESOLVED TYPES: $unresolvedTypes")
        println("RESOLVED TYPES: $resolvedTypes")
        println("GOOD TYPES: ${resolvedTypes - errorTypes}")
        println("ERROR TYPES: $errorTypes")
        println("IMPLICIT TYPES: $implicitTypes")
        println("UNIQUE ERROR TYPES: ${errorTypesReports.size}")



        timePerTransformer.forEach { (transformer, time) ->
            val counter = counterPerTransformer[transformer]!!
            println("${transformer.simpleName}, TIME: ${time * 1e-6} ms, TIME PER FILE: ${(time / counter) * 1e-6} ms, FILES: $counter")
        }
    }
}

fun <T> Collection<T>.progress(label: String, step: Double = 0.1): Sequence<T> {
    val intStep = max(1, (this.size * step).toInt())
    var progress = 0
    return asSequence().onEach {
        if (progress % intStep == 0) {
            println("$label: ${progress * 100 / size}% ($progress/${this.size})")
        }
        progress++
    }
}
