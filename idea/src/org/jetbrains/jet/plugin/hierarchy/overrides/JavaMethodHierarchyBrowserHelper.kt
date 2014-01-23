/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package com.intellij.ide.hierarchy.method

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.ide.util.treeView.NodeDescriptor
import javax.swing.JTree
import java.util.Comparator
import com.intellij.psi.PsiElement

// Proxy methods for access to the protected methods of MethodHierarchyBrowser

fun MethodHierarchyBrowser.getElementFromDescriptorByExtension(descriptor: HierarchyNodeDescriptor): PsiElement? = getElementFromDescriptor(descriptor)

fun MethodHierarchyBrowser.createTreesByExtension(trees: MutableMap<String, JTree>) = createTrees(trees)

fun MethodHierarchyBrowser.getComparatorByExtension(): Comparator<NodeDescriptor<out Any?>>? = getComparator()


