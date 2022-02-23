/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.fir

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtIoFileSourceFile
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.diagnostics.ConeStubDiagnostic
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirGlobalResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.FirResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.FirTransformerBasedResolveProcessor
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.readSourceFileWithMapping
import org.jetbrains.kotlin.utils.addToStdlib.sumByLong
import java.io.File
import java.io.PrintStream
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.reflect.KClass
import kotlin.system.measureNanoTime


fun checkFirProvidersConsistency(firFiles: List<FirFile>) {
    for ((session, files) in firFiles.groupBy { it.moduleData.session }) {
        val provider = session.firProvider as FirProviderImpl
        provider.ensureConsistent(files)
    }
}

private data class FailureInfo(val transformer: KClass<*>, val throwable: Throwable, val file: String)
data class ErrorTypeReport(val report: String, var count: Int = 0)

abstract class BenchListener {
    abstract fun before()
    abstract fun after(stageClass: KClass<*>)
}

class FirResolveBench(val withProgress: Boolean, val listener: BenchListener? = null) {
    data class TotalStatistics(
        val unresolvedTypes: Int,
        val resolvedTypes: Int,
        val errorTypes: Int,
        val implicitTypes: Int,
        val errorFunctionCallTypes: Int,
        val errorQualifiedAccessTypes: Int,
        val fileCount: Int,
        val totalLines: Int,
        val errorTypesReports: Map<String, ErrorTypeReport>,
        val timePerTransformer: Map<String, Measure>
    ) {
        val totalTypes: Int = unresolvedTypes + resolvedTypes
        val goodTypes: Int = resolvedTypes - errorTypes - implicitTypes
        val uniqueErrorTypes: Int = errorTypesReports.size

        val totalMeasure = Measure().apply {
            with(timePerTransformer.values) {
                time = sumByLong { it.time }
                user = sumByLong { it.user }
                cpu = sumByLong { it.cpu }
                gcTime = sumByLong { it.gcTime }
                gcCollections = sumOf { it.gcCollections }
                files = map { it.files }.average().toInt()
            }
        }

        val totalTime: Long get() = totalMeasure.time
    }

    data class Measure(
        var time: Long = 0,
        var user: Long = 0,
        var cpu: Long = 0,
        var gcTime: Long = 0,
        var gcCollections: Int = 0,
        var files: Int = 0,
        var vmCounters: VMCounters = VMCounters()
    )

    val timePerTransformer = mutableMapOf<KClass<*>, Measure>()
    var resolvedTypes = 0
    var errorTypes = 0
    var unresolvedTypes = 0
    var errorFunctionCallTypes = 0
    var errorQualifiedAccessTypes = 0
    var implicitTypes = 0
    var fileCount = 0
    var totalTime = 0L
    var totalLines = 0


    private val fails = mutableListOf<FailureInfo>()
    val hasFiles get() = fails.isNotEmpty()

    private val errorTypesReports = mutableMapOf<String, ErrorTypeReport>()

    fun buildFiles(
        builder: RawFirBuilder,
        ktFiles: List<KtFile>
    ): List<FirFile> {
        listener?.before()
        return ktFiles.map { file ->
            val before = vmStateSnapshot()
            val firFile: FirFile
            val time = measureNanoTime {
                firFile = builder.buildFirFile(file)
                (builder.baseSession.firProvider as FirProviderImpl).recordFile(firFile)
            }
            val after = vmStateSnapshot()
            val diff = after - before
            recordTime(builder::class, diff, time)
            totalLines += StringUtil.countNewLines(file.text)
            firFile
        }.also {
            listener?.after(builder::class)
            totalTime = timePerTransformer.values.sumByLong { it.time }
        }
    }

    fun buildFiles(
        builder: LightTree2Fir,
        files: List<File>
    ): List<FirFile> {
        listener?.before()
        return files.map { file ->
            val before = vmStateSnapshot()
            val firFile: FirFile
            val time = measureNanoTime {
                val sourceFile = KtIoFileSourceFile(file)
                val (code, linesMapping) = with(file.inputStream().reader(Charsets.UTF_8)) {
                    this.readSourceFileWithMapping()
                }
                totalLines += linesMapping.linesCount
                firFile = builder.buildFirFile(code, sourceFile, linesMapping)
                (builder.session.firProvider as FirProviderImpl).recordFile(firFile)
            }
            val after = vmStateSnapshot()
            val diff = after - before
            recordTime(builder::class, diff, time)
            firFile
        }.also {
            listener?.after(builder::class)
            totalTime = timePerTransformer.values.sumByLong { it.time }
        }
    }

    private fun recordTime(stageClass: KClass<*>, diff: VMCounters, time: Long, files: Int = 1) {
        timePerTransformer.computeIfAbsent(stageClass) { Measure() }.apply {
            this.time += time
            this.files += files
            this.user += diff.userTime
            this.cpu += diff.cpuTime
            this.gcCollections += diff.gcInfo.values.sumOf { it.collections.toInt() }
            this.gcTime += diff.gcInfo.values.sumByLong { it.gcTime }
        }
    }

    private fun runStage(processor: FirResolveProcessor, firFileSequence: Sequence<FirFile>) {
        when (processor) {
            is FirTransformerBasedResolveProcessor -> runStage(processor, firFileSequence)
            is FirGlobalResolveProcessor -> runStage(processor, firFileSequence.toList())
        }
    }

    private fun runStage(processor: FirTransformerBasedResolveProcessor, firFileSequence: Sequence<FirFile>) {
        val transformer = processor.transformer
        listener?.before()
        for (firFile in firFileSequence) {
            processWithTimeMeasure(
                transformer::class,
                { transformer.transformFile(firFile, null) }
            ) { e ->
                val ktFile = firFile.psi
                if (ktFile is KtFile) {
                    println("Fail in file: ${ktFile.virtualFilePath}")
                    FailureInfo(transformer::class, e, ktFile.virtualFilePath)
                } else {
                    println("Fail in file: ${firFile.packageFqName} / ${firFile.name}")
                    FailureInfo(transformer::class, e, firFile.packageFqName.asString() + "/" + firFile.name)
                }
            }
        }
        listener?.after(transformer::class)
    }

    private fun runStage(processor: FirGlobalResolveProcessor, firFiles: List<FirFile>) {
        processWithTimeMeasure(
            processor::class,
            { processor.process(firFiles) },
            files = firFiles.size
        ) { e ->
            val message = "Fail on stage ${processor::class}"
            println(message)
            FailureInfo(processor::class, e, message)
        }
    }

    private inline fun processWithTimeMeasure(
        kClass: KClass<*>,
        block: () -> Unit,
        files: Int = 1,
        catchBlock: (Throwable) -> FailureInfo
    ) {
        var fail = false
        val before = vmStateSnapshot()
        val time = measureNanoTime {
            try {
                block()
            } catch (e: Throwable) {
                fails += catchBlock(e)
                fail = true
            }
        }
        if (!fail) {
            val after = vmStateSnapshot()
            val diff = after - before
            recordTime(kClass, diff, time, files)
        }
    }

    fun processFiles(
        firFiles: List<FirFile>,
        processors: List<FirResolveProcessor>
    ) {
        fileCount += firFiles.size
        try {
            for ((_, processor) in processors.withIndex()) {
                //println("Starting stage #$stage. $transformer")
                val firFileSequence = if (withProgress) firFiles.progress("   ~ ") else firFiles.asSequence()
                runStage(processor, firFileSequence)
                checkFirProvidersConsistency(firFiles)
            }

            if (fails.none()) {
                //println("SUCCESS!")
            } else {
                println("ERROR!")
            }
        } finally {

            val fileDocumentManager = FileDocumentManager.getInstance()

            firFiles.forEach {
                it.accept(object : FirDefaultVisitorVoid() {

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
                        val callee = functionCall.calleeReference
                        if (typeRef is FirResolvedTypeRef) {
                            val type = typeRef.type
                            if (type is ConeErrorType) {
                                errorFunctionCallTypes++
                                val psi = callee.psi
                                if (callee is FirErrorNamedReference && psi != null) {
                                    reportProblem(callee.diagnostic.reason, psi)
                                }
                            }
                        }

                        visitElement(functionCall)
                    }

                    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: FirQualifiedAccessExpression) {
                        val typeRef = qualifiedAccessExpression.typeRef
                        val callee = qualifiedAccessExpression.calleeReference
                        if (typeRef is FirResolvedTypeRef) {
                            val type = typeRef.type
                            if (type is ConeErrorType) {
                                errorQualifiedAccessTypes++
                                val psi = callee.psi
                                if (callee is FirErrorNamedReference && psi != null) {
                                    reportProblem(callee.diagnostic.reason, psi)
                                }
                            }
                        }

                        visitElement(qualifiedAccessExpression)
                    }

                    override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
                        visitQualifiedAccessExpression(propertyAccessExpression)
                    }

                    override fun visitTypeRef(typeRef: FirTypeRef) {
                        unresolvedTypes++

                        if (typeRef.psi != null) {
                            if (typeRef is FirErrorTypeRef && typeRef.diagnostic is ConeStubDiagnostic) {
                                return
                            }
                            val psi = typeRef.psi!!
                            val problem = "${typeRef::class.simpleName}: ${typeRef.render()}"
                            reportProblem(problem, psi)
                        }
                    }

                    override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef) {
                        visitTypeRef(implicitTypeRef)
                    }

                    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
                        resolvedTypes++
                        val type = resolvedTypeRef.type
                        if (type is ConeErrorType || type is ConeErrorType) {
                            errorTypes++
                            if (resolvedTypeRef is FirErrorTypeRef && resolvedTypeRef.diagnostic is ConeStubDiagnostic) {
                                return
                            }
                            val psi = resolvedTypeRef.psi ?: return
                            val problem = "${resolvedTypeRef::class.simpleName} -> ${type::class.simpleName}: ${type.render()}"
                            reportProblem(problem, psi)
                        }
                    }
                })
            }
        }


    }

    fun throwFailure() {
        if (fails.any()) {
            val (transformerClass, failure, file) = fails.first()
            throw FirRuntimeException("Failures detected in ${transformerClass.simpleName}, file: $file", failure)
        }
    }

    fun getTotalStatistics(): TotalStatistics = TotalStatistics(
        unresolvedTypes,
        resolvedTypes,
        errorTypes,
        implicitTypes,
        errorFunctionCallTypes,
        errorQualifiedAccessTypes,
        fileCount,
        totalLines,
        errorTypesReports,
        timePerTransformer.mapKeys { (klass, _) -> klass.simpleName!!.toString() }
    )
}

fun doFirResolveTestBench(
    firFiles: List<FirFile>,
    processors: List<FirResolveProcessor>,
    gc: Boolean = true,
    withProgress: Boolean = false,
    silent: Boolean = true
) {

    if (gc) {
        System.gc()
    }

    val bench = FirResolveBench(withProgress)
    bench.processFiles(firFiles, processors)
    if (!silent) bench.getTotalStatistics().report(System.out, "")
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

fun FirResolveBench.TotalStatistics.reportErrors(stream: PrintStream) {
    errorTypesReports.values.sortedByDescending { it.count }.forEach {
        stream.print("${it.count}:")
        stream.println(it.report)
    }
}

fun FirResolveBench.TotalStatistics.reportTimings(stream: PrintStream) {
    printTable(stream) {
        row {
            cell("Stage", LEFT)
            cells("Time", "Time per file", "Files: OK/E/T", "CPU", "User", "GC", "GC count", "L/S")
        }
        separator()
        timePerTransformer.forEach { (transformer, measure) ->
            printMeasureAsTable(measure, this@reportTimings, transformer)
        }

        if (timePerTransformer.keys.isNotEmpty()) {
            separator()
            printMeasureAsTable(totalMeasure, this@reportTimings, "Total time")
        }
    }
}

fun FirResolveBench.TotalStatistics.report(stream: PrintStream, header: String) {
    with(stream) {
        infix fun Int.percentOf(other: Int): String {
            return String.format("%.1f%%", this * 100.0 / other)
        }
        println()
        println("========== $header ==========")
        println("Unresolved (untouched) implicit types: $unresolvedTypes (${unresolvedTypes percentOf totalTypes})")
        println("Resolved types: $resolvedTypes (${resolvedTypes percentOf totalTypes})")
        println("Correctly resolved types: $goodTypes (${goodTypes percentOf resolvedTypes} of resolved)")
        println("Erroneously resolved types: $errorTypes (${errorTypes percentOf resolvedTypes} of resolved)")
        println("   - unresolved calls: $errorFunctionCallTypes")
        println("   - unresolved q.accesses: $errorQualifiedAccessTypes")
        println("Erroneously resolved implicit types: $implicitTypes (${implicitTypes percentOf resolvedTypes} of resolved)")
        println("Unique error types: $uniqueErrorTypes")

        this@report.reportTimings(stream)
    }
}

private fun RTableContext.printMeasureAsTable(measure: FirResolveBench.Measure, statistics: FirResolveBench.TotalStatistics, label: String) {
    val time = measure.time
    val counter = measure.files
    row {
        cell(label, LEFT)
        timeCell(time, fractionDigits = 0)
        timeCell(time / counter)
        cell("$counter/${statistics.fileCount - counter}/${statistics.fileCount}")
        timeCell(measure.cpu, fractionDigits = 0)
        timeCell(measure.user)
        timeCell(measure.gcTime, inputUnit = TableTimeUnit.MS)
        cell(measure.gcCollections.toString())

        linePerSecondCell(statistics.totalLines, time, timeUnit = TableTimeUnit.NS)
    }
}



fun RTableContext.RTableRowContext.linePerSecondCell(linePerSec: Double) {
    val df = DecimalFormat().apply {
        maximumFractionDigits = 1
        isGroupingUsed = true
    }
    cell(df.format(linePerSec))
}
fun RTableContext.RTableRowContext.linePerSecondCell(lines: Int, time: Long, timeUnit: TableTimeUnit = TableTimeUnit.NS) {
    val linePerSec = lines / TableTimeUnit.S.convert(time, from = timeUnit)
    linePerSecondCell(linePerSec)
}


class FirRuntimeException(override val message: String, override val cause: Throwable) : RuntimeException(message, cause)
