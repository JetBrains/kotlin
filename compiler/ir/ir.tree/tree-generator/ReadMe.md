Generates the interfaces and classes of IR elements, as well as visitors, transformers, and the builders of IR declarations
(`org.jetbrains.kotlin.ir.declarations.builder`).

This is done to ensure all implementations are consistent, while avoiding to write boilerplate code manually.

The generator is run on every build, such as when running `./gradlew :dist`.
If you change anything in the model, generator, or the generator code and want to see the result immediately,
you may also run `./gradlew :compiler:ir.tree:generateTree`.

The actual model of IR is declared in [IrTree.kt](src/org/jetbrains/kotlin/ir/generator/IrTree.kt).

Which elements get a builder, and what each of its properties defaults to, is decided in
[BuilderConfigurator.kt](src/org/jetbrains/kotlin/ir/generator/BuilderConfigurator.kt). Every declaration with a leaf
implementation gets one; expressions and the remaining elements are not covered yet.