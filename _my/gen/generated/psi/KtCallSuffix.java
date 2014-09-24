// This is a generated file. Not intended for manual editing.
package generated.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface KtCallSuffix extends PsiElement {

  @Nullable
  KtCharacterConstant getCharacterConstant();

  @Nullable
  KtFloatConstant getFloatConstant();

  @Nullable
  KtIntegerConstant getIntegerConstant();

  @Nullable
  KtNull getNull();

  @Nullable
  KtArrayAccess getArrayAccess();

  @Nullable
  KtBinaryConstant getBinaryConstant();

  @Nullable
  KtCallSuffix getCallSuffix();

  @Nullable
  KtCallableReference getCallableReference();

  @Nullable
  KtDoWhileLoop getDoWhileLoop();

  @Nullable
  KtElvisAccessExpression getElvisAccessExpression();

  @Nullable
  KtForLoop getForLoop();

  @NotNull
  List<KtFunctionLiteralArgument> getFunctionLiteralArgumentList();

  @Nullable
  KtFunctionLiteralExpression getFunctionLiteralExpression();

  @Nullable
  KtIfExpression getIfExpression();

  @Nullable
  KtJumpBreak getJumpBreak();

  @Nullable
  KtJumpContinue getJumpContinue();

  @Nullable
  KtJumpReturn getJumpReturn();

  @Nullable
  KtJumpThrow getJumpThrow();

  @NotNull
  List<KtLabel> getLabelList();

  @Nullable
  KtObjectLiteral getObjectLiteral();

  @Nullable
  KtParenthesizedExpression getParenthesizedExpression();

  @Nullable
  KtPlusPlusAndOthersExpression getPlusPlusAndOthersExpression();

  @Nullable
  KtReferenceExpression getReferenceExpression();

  @Nullable
  KtSafeAccessExpression getSafeAccessExpression();

  @Nullable
  KtStringTemplate getStringTemplate();

  @Nullable
  KtSuperExpression getSuperExpression();

  @Nullable
  KtThisExpression getThisExpression();

  @Nullable
  KtTryBlock getTryBlock();

  @Nullable
  KtTypeArgumentList getTypeArgumentList();

  @Nullable
  KtValueArguments getValueArguments();

  @Nullable
  KtWhen getWhen();

  @Nullable
  KtWhileLoop getWhileLoop();

}
