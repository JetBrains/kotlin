// !DIAGNOSTICS: -EXPOSED_PARAMETER_TYPE -NOTHING_TO_INLINE

// FILE: main.kt
open class AndroidTargetConfigurator :
    Base(),
    ModuleConfiguratorWithTests,
    AndroidModuleConfigurator {

    public inline fun inlineFun(): String {
        return super.classFun() + super<ModuleConfiguratorWithTests>.getConfiguratorSettings() + super<AndroidModuleConfigurator>.getConfiguratorSettings()
    }

    @PublishedApi
    internal inline fun inlineFunPublished(): String {
        return super.classFun() + super<ModuleConfiguratorWithTests>.getConfiguratorSettings() + super<AndroidModuleConfigurator>.getConfiguratorSettings()
    }

    public inline fun inlineFunAnonymousObjects(): String {
        {
            super.classFun() + super<ModuleConfiguratorWithTests>.getConfiguratorSettings() + super<AndroidModuleConfigurator>.getConfiguratorSettings()
        }()

        return object {
            fun run() = super@AndroidTargetConfigurator.classFun() + super<ModuleConfiguratorWithTests>@AndroidTargetConfigurator.getConfiguratorSettings() + super<AndroidModuleConfigurator>@AndroidTargetConfigurator.getConfiguratorSettings()
        }.run()
    }

    public inline fun inlineFunAnonymousNoDiagnostics(): String {
        return object: AndroidTargetConfigurator(), ModuleConfiguratorWithTests, AndroidModuleConfigurator {

            override fun getConfiguratorSettings(): String {
                return super<AndroidTargetConfigurator>.getConfiguratorSettings()
            }

            inline fun anonymousInline() {
                super.classFun() + super<<!QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE!>ModuleConfiguratorWithTests<!>>.getConfiguratorSettings() + super<<!QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE!>AndroidModuleConfigurator<!>>.getConfiguratorSettings()
            }

            fun run() = super.classFun() + super<<!QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE!>ModuleConfiguratorWithTests<!>>.getConfiguratorSettings() + super<<!QUALIFIED_SUPERTYPE_EXTENDED_BY_OTHER_SUPERTYPE!>AndroidModuleConfigurator<!>>.getConfiguratorSettings()
        }.run()
    }



    internal inline fun inlineInternal(): String {
        return super.classFun() + super<ModuleConfiguratorWithTests>.getConfiguratorSettings() + super<AndroidModuleConfigurator>.getConfiguratorSettings()
    }

    private inline fun inlinePrivate(): String {
        return super.classFun() + super<ModuleConfiguratorWithTests>.getConfiguratorSettings() + super<AndroidModuleConfigurator>.getConfiguratorSettings()
    }


    //non-inline
    override fun getConfiguratorSettings() =
        super.classFun() + super<ModuleConfiguratorWithTests>.getConfiguratorSettings() + super<AndroidModuleConfigurator>.getConfiguratorSettings()

    fun noInline() {
        {
            super.classFun() + super<ModuleConfiguratorWithTests>.getConfiguratorSettings() + super<AndroidModuleConfigurator>.getConfiguratorSettings()
        }()

        object {
            fun run() = super@AndroidTargetConfigurator.classFun() + super<ModuleConfiguratorWithTests>@AndroidTargetConfigurator.getConfiguratorSettings() + super<AndroidModuleConfigurator>@AndroidTargetConfigurator.getConfiguratorSettings()
        }.run()
    }

}

open class Base {
    fun classFun(): String = "Class"
}

interface ModuleConfiguratorWithTests : ModuleConfiguratorWithSettings {
    override fun getConfiguratorSettings() = "K"
}

interface ModuleConfiguratorWithSettings  {
    fun getConfiguratorSettings(): String = ""
}


interface AndroidModuleConfigurator :
    ModuleConfiguratorWithSettings {
    override fun getConfiguratorSettings(): String {
        return "O"
    }
}


sealed class FooSealed : Base() {
    class A : FooSealed()

    class B: FooSealed()

    inline fun test() {
        super.classFun()
    }

}

enum class FooEmum {
    A, B {

        inline fun test() {
            super.classFun()
        }
    };

    fun classFun(): String = "Class"
}

class FooOuter : Base() {

    inner class FooInner: Base() {
        inline fun test() {
            super@FooOuter.classFun()
            super.classFun()
        }
    }

}
