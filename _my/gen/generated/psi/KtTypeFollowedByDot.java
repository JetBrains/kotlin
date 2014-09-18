// This is a generated file. Not intended for manual editing.
package generated.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface KtTypeFollowedByDot extends PsiElement {

  @NotNull
  List<KtAnnotation> getAnnotationList();

  @Nullable
  KtNullableType getNullableType();

  @Nullable
  KtParameterModifiersTypeExt getParameterModifiersTypeExt();

  @Nullable
  KtSelfType getSelfType();

  @Nullable
  KtType getType();

  @Nullable
  KtUserType getUserType();

  @Nullable
  KtUserTypeFollowedByDot getUserTypeFollowedByDot();

}
