/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.preprocessor

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.psi.*
import java.io.File


fun main(args: Array<String>) {
    require(args.size() == 1, "Please specify path to sources")

    val sourcePath = File(args.first())

    val configuration = CompilerConfiguration()
    val environment = KotlinCoreEnvironment.createForProduction(Disposable {  }, configuration, emptyList())

    val project = environment.project
    val jetPsiFactory = JetPsiFactory(project)
    val fileType = JetFileType.INSTANCE

    val evaluator = JvmPlatformEvaluator(version = 7)
    //val evaluator = JsPlatformEvaluator()


    println("Using condition evaluator: $evaluator")

    (FileTreeWalk(sourcePath) as Sequence<File>)
            .filter { it.isFile && it.extension == fileType.defaultExtension }
            .forEach { sourceFile ->
                val sourceText = sourceFile.readText().convertLineSeparators()
                val psiFile = jetPsiFactory.createFile(sourceFile.name, sourceText)
                println("$psiFile")

                val visitor = EvaluatorVisitor(evaluator)
                psiFile.accept(visitor)

                var prevIndex = 0
                val resultText = StringBuilder()
                for ((range, selector) in visitor.elementModifications) {
                    resultText.append(sourceText, prevIndex, range.startOffset)
                    val rangeText = range.substring(sourceText)
                    val newValue = selector(rangeText)
                    if (newValue.isEmpty()) {
                        resultText.append("/* Not available on ${visitor.evaluator} */")
                        repeat(StringUtil.getLineBreakCount(rangeText)) {
                            resultText.append("\n")
                        }
                    }
                    else {
                        resultText.append(newValue)
                    }
                    prevIndex = range.endOffset
                }
                resultText.append(sourceText, prevIndex, sourceText.length())

                println(resultText.toString())
                //processDeclaration("/", psiFile, evaluator)
            }
}


class EvaluatorVisitor(val evaluator: Evaluator) : JetTreeVisitorVoid() {

    val elementModifications: MutableList<Pair<TextRange, (String) -> String>> = arrayListOf()


    override fun visitDeclaration(declaration: JetDeclaration) {
        super.visitDeclaration(declaration)

        val annotations = declaration.parseConditionalAnnotations()
        val name = (declaration as? JetNamedDeclaration)?.nameAsSafeName ?: declaration.name
        val conditionalResult = evaluator(annotations)
        println("declaration: ${declaration.javaClass.simpleName} $name, annotations: ${annotations.joinToString { it.toString() }}, evaluation result: $conditionalResult")
        if (!conditionalResult)
            elementModifications.add(declaration.textRange to {it -> ""})
        else {
            val targetName = annotations.filterIsInstance<Conditional.TargetName>().singleOrNull()
            if (targetName != null) {
                val placeholderName = (declaration as JetNamedDeclaration).nameAsName!!.asString()
                val realName = targetName.name
                elementModifications.add(declaration.textRange to { it -> it.replace(placeholderName, realName) })
            }
        }
    }
}


val JetAnnotationEntry.typeReferenceName: String? get() =
        (typeReference?.typeElement as? JetUserType)?.referencedName

fun String.convertLineSeparators(): String = StringUtil.convertLineSeparators(this)



