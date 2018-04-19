/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.idea.refactoring.toPsiFile
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtVisitorVoid
import java.util.*
import kotlin.system.measureNanoTime

class AllKotlinLightClassTest : WholeProjectPerformanceTest(), WholeProjectKotlinFileProvider {

    override fun doTest(file: VirtualFile): PerFileTestResult {
        val results = mutableMapOf<String, Long>()
        var totalNs = 0L

        val psiFile = file.toPsiFile(project) ?: run {
            return WholeProjectPerformanceTest.PerFileTestResult(results, totalNs, listOf(AssertionError("PsiFile not found for $file")))
        }

        val errors = mutableListOf<Throwable>()

        fun buildAllLightClasses(name: String, predicate: (KtClassOrObject) -> Boolean) {
            val result = measureNanoTime {
                try {
                    // Build light class by PsiFile
                    psiFile.acceptRecursively(object : KtVisitorVoid() {
                        override fun visitClassOrObject(classOrObject: KtClassOrObject) {
                            if (!predicate(classOrObject)) return
                            val lightClass = classOrObject.toLightClass() as? KtLightClassForSourceDeclaration ?: return
                            Arrays.hashCode(lightClass.superTypes)
                            Arrays.hashCode(lightClass.fields)
                            Arrays.hashCode(lightClass.methods)
                            lightClass.hashCode()
                        }
                    })

                } catch (t: Throwable) {
                    t.printStackTrace()
                    errors += t
                }
            }
            results[name] = result
            totalNs += result
        }

        buildAllLightClasses("LightClasses_Top") {
            it.containingDeclarationForPseudocode == null
        }

        buildAllLightClasses("LightClasses_Members") {
            !it.isLocal && it.containingDeclarationForPseudocode is KtClassOrObject
        }

        return PerFileTestResult(results, totalNs, errors)
    }
}