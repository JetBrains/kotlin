import a.*

val KtDeclaration.fqNameWithoutCompanions: KtFile
    get() = containingKtFile