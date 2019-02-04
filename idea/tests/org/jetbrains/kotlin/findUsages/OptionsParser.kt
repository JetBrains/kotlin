/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
                        else -> throw IllegalStateException("Invalid option: $s")
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
                        else -> throw IllegalStateException("Invalid option: $s")
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
                        else -> throw IllegalStateException("Invalid option: $s")
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
                        else -> throw IllegalStateException("Invalid option: $s")
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
                        else -> throw IllegalStateException("Invalid option: $s")
                    }
                }
            }
        }
    },
    JAVA_FIELD {
        override fun parse(text: String, project: Project): FindUsagesOptions {
            return JavaVariableFindUsagesOptions(project).apply {
                for (s in InTextDirectivesUtils.findListWithPrefixes(text, "// OPTIONS: ")) {
                    if (parseCommonOptions(this, s)) continue

                    when (s) {
                        "skipRead" -> isReadAccess = false
                        "skipWrite" -> isWriteAccess = false
                        else -> throw IllegalStateException("Invalid option: `$s`")
                    }
                }
            }
        }
    },
    JAVA_PACKAGE {
        override fun parse(text: String, project: Project): FindUsagesOptions {
            return JavaPackageFindUsagesOptions(project).apply {
                for (s in InTextDirectivesUtils.findListWithPrefixes(text, "// OPTIONS: ")) {
                    if (parseCommonOptions(this, s)) continue

                    throw IllegalStateException("Invalid option: `$s`")
                }
            }
        }
    },
    DEFAULT {
        override fun parse(text: String, project: Project): FindUsagesOptions {
            return FindUsagesOptions(project).apply {
                for (s in InTextDirectivesUtils.findListWithPrefixes(text, "// OPTIONS: ")) {
                    if (parseCommonOptions(this, s)) continue

                    throw IllegalStateException("Invalid option: `$s`")
                }
            }
        }
    };

    abstract fun parse(text: String, project: Project): FindUsagesOptions

    companion object {
        protected fun parseCommonOptions(options: JavaFindUsagesOptions, s: String): Boolean {
            if (parseCommonOptions(options as FindUsagesOptions, s)) {
                return true
            }

            return when (s) {
                "skipImports" -> {
                    options.isSkipImportStatements = true
                    true
                }
                else -> false
            }
        }


        protected fun parseCommonOptions(options: FindUsagesOptions, s: String): Boolean {
            return when (s) {
                "usages" -> {
                    options.isUsages = true
                    true
                }
                "textOccurrences" -> {
                    options.isSearchForTextOccurrences = true
                    true
                }
                else -> false
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