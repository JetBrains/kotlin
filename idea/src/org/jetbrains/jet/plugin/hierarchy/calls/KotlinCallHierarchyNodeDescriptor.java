package org.jetbrains.jet.plugin.hierarchy.calls;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.ide.hierarchy.JavaHierarchyUtil;
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.util.CompositeAppearance;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import javax.swing.*;
import java.awt.*;

public class KotlinCallHierarchyNodeDescriptor extends HierarchyNodeDescriptor implements Navigatable {
    private int usageCount = 1;
    private final CallHierarchyNodeDescriptor javaDelegate;

    public KotlinCallHierarchyNodeDescriptor(@NotNull Project project,
            HierarchyNodeDescriptor parentDescriptor,
            @NotNull PsiElement element,
            boolean isBase,
            boolean navigateToReference) {
        super(project, parentDescriptor, element, isBase);
        this.javaDelegate = new CallHierarchyNodeDescriptor(myProject, null, element, isBase, navigateToReference);
    }

    public final CallHierarchyNodeDescriptor getJavaDelegate() {
        return javaDelegate;
    }

    public final void incrementUsageCount(){
        usageCount++;
    }

    public final PsiElement getTargetElement(){
        return myElement;
    }

    @Override
    public final boolean isValid(){
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

        PsiElement targetElement = getTargetElement();
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

        String packageName = null;
        if (targetElement instanceof JetElement) {
            packageName = JetPsiUtil.getPackageName((JetElement) targetElement);
        }
        else {
            PsiClass enclosingClass = PsiTreeUtil.getParentOfType(targetElement, PsiClass.class, false);
            if (enclosingClass != null) {
                packageName = JavaHierarchyUtil.getPackageName(enclosingClass);
            }
        }

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

        if (element instanceof JetFile) {
            elementText = ((JetFile) element).getName();
        }
        else if (element instanceof JetNamedDeclaration) {
            BindingContext bindingContext =
                    AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) element.getContainingFile()).getBindingContext();

            DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
            if (descriptor == null) return null;

            if (element instanceof JetClassOrObject) {
                if (element instanceof JetObjectDeclaration && ((JetObjectDeclaration) element).isClassObject()) {
                    descriptor = descriptor.getContainingDeclaration();
                    if (!(descriptor instanceof ClassDescriptor)) return null;

                    elementText = renderClassOrObject((ClassDescriptor) descriptor);
                }
                else if (element instanceof JetEnumEntry) {
                    elementText = ((JetEnumEntry) element).getName();
                }
                else {
                    if (((JetClassOrObject) element).getName() != null) {
                        elementText = renderClassOrObject((ClassDescriptor) descriptor);
                    }
                    else {
                        elementText = "[anonymous]";
                    }
                }
            }
            else if (element instanceof JetNamedFunction) {
                elementText = renderNamedFunction((FunctionDescriptor) descriptor);
            }
            else if (element instanceof JetProperty) {
                elementText = ((JetProperty) element).getName();
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

    private static String renderNamedFunction(FunctionDescriptor descriptor) {
        String name = descriptor.getName().asString();
        String paramTypes = StringUtil.join(
                descriptor.getValueParameters(),
                new Function<ValueParameterDescriptor, String>() {
                    @Override
                    public String fun(ValueParameterDescriptor descriptor) {
                        return DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(descriptor.getType());
                    }
                },
                ", "
        );
        return name + "(" + paramTypes + ")";
    }

    private static String renderClassOrObject(ClassDescriptor descriptor) {
        return descriptor.getName().asString();
    }

    @Override
    public void navigate(boolean requestFocus) {
        if (myElement instanceof Navigatable) {
            ((Navigatable)myElement).navigate(requestFocus);
        }
    }

    @Override
    public boolean canNavigate() {
        return myElement instanceof Navigatable && ((Navigatable) myElement).canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return canNavigate();
    }
}
