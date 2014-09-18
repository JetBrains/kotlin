// This is a generated file. Not intended for manual editing.
package generated.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface KtSafeAccessExpression extends PsiElement {

  @NotNull
  List<KtFloatConstant> getFloatConstantList();

  @NotNull
  List<KtIntegerConstant> getIntegerConstantList();

  @Nullable
  KtArrayAccess getArrayAccess();

  @NotNull
  List<KtBinaryConstant> getBinaryConstantList();

  @Nullable
  KtCallSuffix getCallSuffix();

  @NotNull
  List<KtCallableReference> getCallableReferenceList();

  @Nullable
  KtElvisAccessExpression getElvisAccessExpression();

  @NotNull
  List<KtFunctionLiteralExpression> getFunctionLiteralExpressionList();

  @NotNull
  List<KtIfExpression> getIfExpressionList();

  @NotNull
  List<KtJumpBreak> getJumpBreakList();

  @NotNull
  List<KtJumpContinue> getJumpContinueList();

  @NotNull
  List<KtJumpReturn> getJumpReturnList();

  @NotNull
  List<KtJumpThrow> getJumpThrowList();

  @NotNull
  List<KtLabel> getLabelList();

  @NotNull
  List<KtLoop> getLoopList();

  @NotNull
  List<KtObjectLiteral> getObjectLiteralList();

  @NotNull
  List<KtParenthesizedExpression> getParenthesizedExpressionList();

  @NotNull
  List<KtReferenceExpression> getReferenceExpressionList();

  @Nullable
  KtSafeAccessExpression getSafeAccessExpression();

  @NotNull
  List<KtStringTemplate> getStringTemplateList();

  @NotNull
  List<KtThisExpression> getThisExpressionList();

  @NotNull
  List<KtTryBlock> getTryBlockList();

  @Nullable
  KtType getType();

  @NotNull
  List<KtWhen> getWhenList();

}
