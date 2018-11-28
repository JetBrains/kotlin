/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.findUsages

import com.intellij.find.findUsages.*
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import org.jetbrains.kotlin.idea.findUsages.KotlinClassFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinFunctionFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinPropertyFindUsagesOptions
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.test.InTextDirectivesUtils

internal enum class OptionsParser {
    CLASS {
        override fun parse(text: String, project: Project): FindUsagesOptions {
            return KotlinClassFindUsagesOptions(project).apply {
                isUsages = false
                isSearchForTextOccurrences = false
                searchConstructorUsages = false
                for (s in InTextDirectivesUtils.findListWithPrefixes(text, "// OPTIONS: ")) {
                    if (parseCommonOptions(this, s)) continue

                    when (s) {
                        "constructorUsages" -> searchConstructorUsages = true
                        "derivedInterfaces" -> isDerivedInterfaces = true
                        "derivedClasses" -> isDerivedClasses = true
                        "functionUsages" -> isMethodsUsages = true
                        "propertyUsages" -> isFieldsUsages = true
                        "expected" -> searchExpected = true
                        else -> throw IllegalStateException("Invalid option: " + s)
                    }
                }
            }
        }
    },
    FUNCTION {
        override fun parse(text: String, project: Project): FindUsagesOptions {
            return KotlinFunctionFindUsagesOptions(project).apply {
                isUsages = false
                for (s in InTextDirectivesUtils.findListWithPrefixes(text, "// OPTIONS: ")) {
                    if (parseCommonOptions(this, s)) continue

                    when (s) {
                        "overrides" -> {
                            isOverridingMethods = true
                            isImplementingMethods = true
                        }
                        "overloadUsages" -> {
                            isIncludeOverloadUsages = true
                            isUsages = true
                        }
                        "expected" -> {
                            searchExpected = true
                            isUsages = true
                        }
                        else -> throw IllegalStateException("Invalid option: " + s)
                    }
                }
            }
        }
    },
    PROPERTY {
        override fun parse(text: String, project: Project): FindUsagesOptions {
            return KotlinPropertyFindUsagesOptions(project).apply {
                isUsages = false
                for (s in InTextDirectivesUtils.findListWithPrefixes(text, "// OPTIONS: ")) {
                    if (parseCommonOptions(this, s)) continue

                    when (s) {
                        "overrides" -> searchOverrides = true
                        "skipRead" -> isReadAccess = false
                        "skipWrite" -> isWriteAccess = false
                        "expected" -> searchExpected = true
                        else -> throw IllegalStateException("Invalid option: " + s)
                    }
                }
            }
        }
    },
    JAVA_CLASS {
        override fun parse(text: String, project: Project): FindUsagesOptions {
            return KotlinClassFindUsagesOptions(project).apply {
                isUsages = false
                searchConstructorUsages = false
                for (s in InTextDirectivesUtils.findListWithPrefixes(text, "// OPTIONS: ")) {
                    if (parseCommonOptions(this, s)) continue

                    when (s) {
                        "derivedInterfaces" -> isDerivedInterfaces = true
                        "derivedClasses" -> isDerivedClasses = true
                        "implementingClasses" -> isImplementingClasses = true
                        "methodUsages" -> isMethodsUsages = true
                        "fieldUsages" -> isFieldsUsages = true
                        else -> throw IllegalStateException("Invalid option: " + s)
                    }
                }
            }
        }
    },
    JAVA_METHOD {
        override fun parse(text: String, project: Project): FindUsagesOptions {
            return JavaMethodFindUsagesOptions(project).apply {
                isUsages = false
                for (s in InTextDirectivesUtils.findListWithPrefixes(text, "// OPTIONS: ")) {
                    if (parseCommonOptions(this, s)) continue

                    when (s) {
                        "overrides" -> {
                            isOverridingMethods = true
                            isImplementingMethods = true
                        }
                        else -> throw IllegalStateException("Invalid option: " + s)
                    }
                }
            }
        }
    },
    JAVA_FIELD {
        override fun parse(text: String, project: Project): FindUsagesOptions {
            return JavaVariableFindUsagesOptions(project)
        }
    },
    JAVA_PACKAGE {
        override fun parse(text: String, project: Project): FindUsagesOptions {
            return JavaPackageFindUsagesOptions(project)
        }
    },
    DEFAULT {
        override fun parse(text: String, project: Project): FindUsagesOptions {
            return FindUsagesOptions(project)
        }
    };

    abstract fun parse(text: String, project: Project): FindUsagesOptions

    companion object {
        protected fun parseCommonOptions(options: JavaFindUsagesOptions, s: String): Boolean {
            when (s) {
                "usages" -> {
                    options.isUsages = true
                    return true
                }
                "skipImports" -> {
                    options.isSkipImportStatements = true
                    return true
                }
                "textOccurrences" -> {
                    options.isSearchForTextOccurrences = true
                    return true
                }
                else -> return false
            }

        }

        fun getParserByPsiElementClass(klass: Class<out PsiElement>): OptionsParser? {
            return when (klass) {
                KtNamedFunction::class.java -> FUNCTION
                KtProperty::class.java, KtParameter::class.java -> PROPERTY
                KtClass::class.java -> CLASS
                PsiMethod::class.java -> JAVA_METHOD
                PsiClass::class.java -> JAVA_CLASS
                PsiField::class.java -> JAVA_FIELD
                PsiPackage::class.java -> JAVA_PACKAGE
                KtTypeParameter::class.java -> DEFAULT
                else -> null
            }
        }
    }
}