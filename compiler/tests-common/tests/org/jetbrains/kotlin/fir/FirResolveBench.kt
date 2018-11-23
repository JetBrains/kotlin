/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.fir

import com.intellij.openapi.fileEditor.FileDocumentManager
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinErrorType
import org.jetbrains.kotlin.fir.types.FirResolvedType
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import kotlin.reflect.KClass
import kotlin.system.measureNanoTime

fun doFirResolveTestBench(firFiles: List<FirFile>, transformers: List<FirTransformer<Nothing?>>) {

    System.gc()

    val timePerTransformer = mutableMapOf<KClass<*>, Long>()
    val counterPerTransformer = mutableMapOf<KClass<*>, Long>()
    val totalLength = 0
    var resolvedTypes = 0
    var errorTypes = 0
    var unresolvedTypes = 0


    try {
        for (transformer in transformers) {
            for (firFile in firFiles) {
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

                override fun visitType(type: FirType) {
                    unresolvedTypes++
                }

                override fun visitResolvedType(resolvedType: FirResolvedType) {
                    resolvedTypes++
                    val type = resolvedType.type
                    if (type is ConeKotlinErrorType || type is ConeClassErrorType) {
                        if (resolvedType.psi == null) {
                            implicitTypes++
                        } else {
                            val psi = resolvedType.psi!!
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