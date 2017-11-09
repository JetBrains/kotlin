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

package org.jetbrains.kotlin.idea.hierarchy.calls;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.roots.ui.util.CompositeAppearance;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.ui.LayeredIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionUtils;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class KotlinCallHierarchyNodeDescriptor extends HierarchyNodeDescriptor implements Navigatable {
    private int usageCount = 1;
    private final Set<PsiReference> references = new HashSet<>();
    private final CallHierarchyNodeDescriptor javaDelegate;

    public KotlinCallHierarchyNodeDescriptor(
            @Nullable HierarchyNodeDescriptor parentDescriptor,
            @NotNull KtElement element,
            boolean isBase,
            boolean navigateToReference) {
        super(element.getProject(), parentDescriptor, element, isBase);
        this.javaDelegate = new CallHierarchyNodeDescriptor(myProject, null, element, isBase, navigateToReference);
    }

    public final void incrementUsageCount() {
        usageCount++;
        javaDelegate.incrementUsageCount();
    }

    public final void addReference(PsiReference reference) {
        references.add(reference);
        javaDelegate.addReference(reference);
    }

    @Override
    public final boolean isValid(){
        //noinspection ConstantConditions
        PsiElement myElement = getPsiElement();
        return myElement != null && myElement.isValid();
    }

    @Override
    public final boolean update(){
        CompositeAppearance oldText = myHighlightedText;
        Icon oldIcon = getIcon();

        int flags = Iconable.ICON_FLAG_VISIBILITY;
        if (isMarkReadOnly()) {
            flags |= Iconable.ICON_FLAG_READ_STATUS;
        }

        boolean changes = super.update();

        PsiElement targetElement = getPsiElement();
        String elementText = renderElement(targetElement);

        if (elementText == null) {
            String invalidPrefix = IdeBundle.message("node.hierarchy.invalid");
            if (!myHighlightedText.getText().startsWith(invalidPrefix)) {
                myHighlightedText.getBeginning().addText(invalidPrefix, HierarchyNodeDescriptor.getInvalidPrefixAttributes());
            }
            return true;
        }

        Icon newIcon = targetElement.getIcon(flags);
        if (changes && myIsBase) {
            LayeredIcon icon = new LayeredIcon(2);
            icon.setIcon(newIcon, 0);
            icon.setIcon(AllIcons.Hierarchy.Base, 1, -AllIcons.Hierarchy.Base.getIconWidth() / 2, 0);
            newIcon = icon;
        }
        setIcon(newIcon);

        myHighlightedText = new CompositeAppearance();
        TextAttributes mainTextAttributes = null;
        if (myColor != null) {
            mainTextAttributes = new TextAttributes(myColor, null, null, null, Font.PLAIN);
        }

        String packageName = KtPsiUtil.getPackageName((KtElement) targetElement);

        myHighlightedText.getEnding().addText(elementText, mainTextAttributes);

        if (usageCount > 1) {
            myHighlightedText.getEnding().addText(
                    IdeBundle.message("node.call.hierarchy.N.usages", usageCount),
                    HierarchyNodeDescriptor.getUsageCountPrefixAttributes()
            );
        }

        if (packageName == null) {
            packageName = "";
        }
        myHighlightedText.getEnding().addText("  (" + packageName + ")", HierarchyNodeDescriptor.getPackageNameAttributes());

        myName = myHighlightedText.getText();

        if (!(Comparing.equal(myHighlightedText, oldText) && Comparing.equal(getIcon(), oldIcon))) {
            changes = true;
        }
        return changes;
    }

    @Nullable
    private static String renderElement(@Nullable PsiElement element) {
        String elementText;
        String containerText = null;

        if (element instanceof KtFile) {
            elementText = ((KtFile) element).getName();
        }
        else if (element instanceof KtNamedDeclaration) {
            BindingContext bindingContext = ResolutionUtils.analyze((KtElement) element, BodyResolveMode.FULL);

            DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
            if (descriptor == null) return null;

            if (element instanceof KtClassOrObject) {
                if (element instanceof KtObjectDeclaration && ((KtObjectDeclaration) element).isCompanion()) {
                    descriptor = descriptor.getContainingDeclaration();
                    if (!(descriptor instanceof ClassDescriptor)) return null;

                    elementText = renderClassOrObject((ClassDescriptor) descriptor);
                }
                else if (element instanceof KtEnumEntry) {
                    elementText = ((KtEnumEntry) element).getName();
                }
                else {
                    if (((KtClassOrObject) element).getName() != null) {
                        elementText = renderClassOrObject((ClassDescriptor) descriptor);
                    }
                    else {
                        elementText = "[anonymous]";
                    }
                }
            }
            else if (element instanceof KtNamedFunction || element instanceof KtConstructor) {
                elementText = renderNamedFunction((FunctionDescriptor) descriptor);
            }
            else if (element instanceof KtProperty) {
                elementText = ((KtProperty) element).getName();
            }
            else return null;

            DeclarationDescriptor containerDescriptor = descriptor.getContainingDeclaration();
            while (containerDescriptor != null) {
                String name = containerDescriptor.getName().asString();
                if (!name.startsWith("<")) {
                    containerText = name;
                    break;
                }
                containerDescriptor = containerDescriptor.getContainingDeclaration();
            }
        }
        else return null;

        if (elementText == null) return null;
        return containerText != null ? containerText + "." + elementText : elementText;
    }

    public static String renderNamedFunction(FunctionDescriptor descriptor) {
        DeclarationDescriptor descriptorForName = descriptor instanceof ConstructorDescriptor
                                                  ? descriptor.getContainingDeclaration()
                                                  : descriptor;
        String name = descriptorForName.getName().asString();
        String paramTypes = StringUtil.join(
                descriptor.getValueParameters(),
                descriptor1 -> DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(descriptor1.getType()),
                ", "
        );
        return name + "(" + paramTypes + ")";
    }

    private static String renderClassOrObject(ClassDescriptor descriptor) {
        return descriptor.getName().asString();
    }

    @Override
    public void navigate(boolean requestFocus) {
        javaDelegate.navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return javaDelegate.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return javaDelegate.canNavigateToSource();
    }
}
