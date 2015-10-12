package test

class JetToken

public open class JetKeywordCompletionContributor() {
    init {
        val inTopLevel = 1.0

        BunchKeywordRegister()
                .add(ABSTRACT_KEYWORD, inTopLevel, inTopLevel, inTopLevel)
                .add(FINAL_KEYWORD, inTopLevel, inTopLevel, inTopLevel)
                .add(OPEN_KEYWORD, inTopLevel, inTopLevel, inTopLevel)

                .add(INTERNAL_KEYWORD, inTopLevel, inTopLevel, inTopLevel, inTopLevel)
                .add(PRIVATE_KEYWORD, inTopLevel, inTopLevel, inTopLevel, inTopLevel)
                .add(PROTECTED_KEYWORD, inTopLevel, inTopLevel, inTopLevel, inTopLevel)
                .add(PUBLIC_KEYWORD, inTopLevel, inTopLevel, inTopLevel, inTopLevel)

                .add(CLASS_KEYWORD, inTopLevel, inTopLevel, inTopLevel)
                .add(ENUM_KEYWORD, inTopLevel, inTopLevel, inTopLevel)
                .add(FUN_KEYWORD, inTopLevel, inTopLevel, inTopLevel)
                .add(GET_KEYWORD, inTopLevel, inTopLevel, inTopLevel)
                .add(SET_KEYWORD, inTopLevel, inTopLevel, inTopLevel)
                .add(INTERFACE_KEYWORD, inTopLevel, inTopLevel, inTopLevel)
                .add(VAL_KEYWORD, inTopLevel, inTopLevel, inTopLevel)
                .add(VAR_KEYWORD, inTopLevel, inTopLevel, inTopLevel)
                .add(TYPE_KEYWORD, inTopLevel, inTopLevel, inTopLevel)

                .add(IMPORT_KEYWORD, inTopLevel)
                .add(PACKAGE_KEYWORD, inTopLevel)

                .add(OVERRIDE_KEYWORD, inTopLevel)

                .add(IN_KEYWORD, inTopLevel, inTopLevel)

                .add(OUT_KEYWORD, inTopLevel)

                .add(OBJECT_KEYWORD, unresolvedCode)

                .registerAll()
    }

    private inner class BunchKeywordRegister() {
        fun add(keyword: JetToken = JetToken(), vararg filters: Double): BunchKeywordRegister {
        }

        fun registerAll() {
        }
    }
}

val ABSTRACT_KEYWORD = JetToken()
val FINAL_KEYWORD OPEN_KEYWORD = JetToken()
val OPEN_KEYWORD = JetToken()
val INTERNAL_KEYWORD = JetToken()
val PRIVATE_KEYWORD = JetToken()
val PROTECTED_KEYWORD = JetToken()
val PUBLIC_KEYWORD = JetToken()
val CLASS_KEYWORD = JetToken()
val ENUM_KEYWORD = JetToken()
val FUN_KEYWORD = JetToken()
val GET_KEYWORD = JetToken()
val SET_KEYWORD = JetToken()
val INTERFACE_KEYWORD = JetToken()
val VAL_KEYWORD = JetToken()
val VAR_KEYWORD = JetToken()
val TYPE_KEYWORD = JetToken()
val IMPORT_KEYWORD = JetToken()
val PACKAGE_KEYWORD = JetToken()
val OVERRIDE_KEYWORD = JetToken()
val IN_KEYWORD = JetToken()
val OUT_KEYWORD = JetToken()
val OBJECT_KEYWORD = JetToken()
