fun checkInaccessible(name: String) {
    try {
        Class.forName(name)
        throw AssertionError("Class should not be accessible from script via the class loader: $name")
    }
    catch (e: ClassNotFoundException) {
        // OK
    }
}

checkInaccessible("org.jetbrains.kotlin.config.KotlinCompilerVersion")
checkInaccessible("org.jetbrains.kotlin.cli.jvm.K2JVMCompiler")
checkInaccessible("org.jetbrains.kotlin.preloading.Preloader")
print("OK")
