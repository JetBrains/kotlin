package org.jetbrains.jet.lang.annotations;

import com.google.common.collect.Lists;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Function;
import com.intellij.util.PsiNavigateUtil;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.FunctionDescriptor;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class JetLineMarkerProvider implements LineMarkerProvider {

    public static final Icon OVERRIDING_FUNCTION = IconLoader.getIcon("/general/overridingMethod.png");

    @Override
    public LineMarkerInfo getLineMarkerInfo(PsiElement element) {
        if (element instanceof JetFunction) {
            JetFunction jetFunction = (JetFunction) element;

            JetFile file = PsiTreeUtil.getParentOfType(element, JetFile.class);
            assert file != null;
            final BindingContext bindingContext = AnalyzingUtils.analyzeFile(file);
            FunctionDescriptor functionDescriptor = bindingContext.getFunctionDescriptor(jetFunction);
            final Set<? extends FunctionDescriptor> overriddenFunctions = functionDescriptor.getOverriddenFunctions();
            if (!overriddenFunctions.isEmpty()) {
                return new LineMarkerInfo<JetFunction>(
                        jetFunction, jetFunction.getTextOffset(), OVERRIDING_FUNCTION, Pass.UPDATE_ALL,
                        new Function<JetFunction, String>() {
                            @Override
                            public String fun(JetFunction jetFunction) {
                                return overriddenFunctions.toString();
                            }
                        },
                        new GutterIconNavigationHandler<JetFunction>() {
                            @Override
                            public void navigate(MouseEvent event, JetFunction elt) {
                                final List<PsiElement> list = Lists.newArrayList();
                                for (FunctionDescriptor overriddenFunction : overriddenFunctions) {
                                    PsiElement declarationPsiElement = bindingContext.getDeclarationPsiElement(overriddenFunction);
                                    list.add(declarationPsiElement);
                                }
                                if (list.isEmpty()) {
                                    String myEmptyText = "empty text";
                                    final JComponent renderer = HintUtil.createErrorLabel(myEmptyText);
                                    final JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(renderer, renderer).createPopup();
                                    if (event != null) {
                                        popup.show(new RelativePoint(event));
                                    }
                                    return;
                                }
                                if (list.size() == 1) {
                                    PsiNavigateUtil.navigate(list.iterator().next());
                                }
                                else {
                                    final JBPopup popup = NavigationUtil.getPsiElementPopup(PsiUtilBase.toPsiElementArray(list), new DefaultPsiElementCellRenderer() {
                                                @Override
                                                public String getElementText(PsiElement element) {
                                                    if (element instanceof JetFunction) {
                                                        JetFunction function = (JetFunction) element;
                                                        return bindingContext.getFunctionDescriptor(function).toString();
                                                    }
                                                    return super.getElementText(element);
                                                }
                                            }, "title");
                                    if (event != null) {
                                        popup.show(new RelativePoint(event));
                                    }
                                }
                            }
                        }
                );
            }
        }
        return null;
    }

    @Override
    public void collectSlowLineMarkers(List<PsiElement> elements, Collection<LineMarkerInfo> result) {
    }
}
