package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.actions.MethodSmartStepTarget
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.util.Range
import org.jetbrains.kotlin.psi.JetElement

public class KotlinMethodSmartStepTarget(val resolvedElement: JetElement,
                                         psiMethod: PsiMethod,
                                         label: String?,
                                         highlightElement: PsiElement,
                                         needBreakpointRequest: Boolean,
                                         lines: Range<Int>
): MethodSmartStepTarget(psiMethod, label, highlightElement, needBreakpointRequest, lines)