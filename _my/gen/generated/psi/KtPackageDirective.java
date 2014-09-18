// This is a generated file. Not intended for manual editing.
package generated.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface KtPackageDirective extends PsiElement {

  @Nullable
  KtDotIdentifier getDotIdentifier();

  @NotNull
  List<KtClassDeclaration> getClassDeclarationList();

  @NotNull
  List<KtFunction> getFunctionList();

  @NotNull
  List<KtImportDirective> getImportDirectiveList();

  @NotNull
  List<KtObject> getObjectList();

  @NotNull
  List<KtPackageDirective> getPackageDirectiveList();

  @NotNull
  List<KtProperty> getPropertyList();

  @Nullable
  KtReferenceExpression getReferenceExpression();

  @NotNull
  List<KtTypedef> getTypedefList();

}
