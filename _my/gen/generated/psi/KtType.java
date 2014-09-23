// This is a generated file. Not intended for manual editing.
package generated.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface KtType extends PsiElement {

  @NotNull
  List<KtAnnotationEntry> getAnnotationEntryList();

  @Nullable
  KtFunctionType getFunctionType();

  @Nullable
  KtFunctionTypeReceiverReference getFunctionTypeReceiverReference();

  @NotNull
  List<KtLeftFunctionType> getLeftFunctionTypeList();

  @NotNull
  List<KtLongAnnotation> getLongAnnotationList();

  @Nullable
  KtNullableType getNullableType();

  @Nullable
  KtSelfType getSelfType();

  @Nullable
  KtSimpleUserType getSimpleUserType();

  @Nullable
  KtSimpleUserTypeAdd getSimpleUserTypeAdd();

}
