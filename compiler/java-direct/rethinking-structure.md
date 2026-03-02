
# Rethinking Structure due to new dependencies findings

## Status

We developed the approach to the JavaFacade replacement (see IMPLEMENTATION_PLAN.md and other relevant documents in the same folder).
But after some progress in implementing the plan, realized that something important is missing, that do not allow us to
see the java symbols during Kotlin resolution. It it first of all visible on the new JavaClassFinder itself, since
we are trying to reuse the FIR infrastructure to resolve external symbols, but I expect it to surface in the resolution
of the non-Kotlin symbols from binary representation in the Kotlin itself.

The root of the problem is that in the scheme that we're trying to replace, the JavaFacade, in addition to being the access point
to the Java classes created from sources, also sits on top and provides an access to the binary java classes, that are used everywhere in
kotlin resolution. And all this infrastructure is highly dependent on the VirtualFile, which is quite heavy IJ Platform bound interface.

So, now the first thing we need to do is to understand the current structure of interdependent class finders and infrastucture below and above them,
to be able to find a good way forward.

It is quite likely that the first implementation will need to rely on Virtual files still, to speed up implementation. But we need to see a path
forward that would allow us to drop the IJ platform dependency completely.
But even for the first step, we need to understand which parts of the Platform we're pulling and whether we can minimize the dependencies already.

Analyze the current compiler code, and provide a comprehensive analysis of what paths are we taking and which parts of the platform we are using
when we perform the resolution of external dependencies, e.g.:
- an FQN to resolve -> KotlinClassFinder -> (some path) -> VirtualFile -> check metadata -> deserialize Kotlin class or Java Binary class
- an external FQN to resolve -> JavaClassFinderImpl -> (some path) -> VirtualFile -> binary Java class

We need to understand, at what places we're starting to call the platform (`org.intellij.*` packages) or classes that are based on platform services
(caches, VirtualFile, PSI, VFS, etc.), to understand how the resolution of the external dependencies is performed, and how it should be 
performed if we replace the JavaClassFinder with the implementation from the `java-direct` module.

Use `testGenericSamProjectedOut` as a test bed. It is a bit more complicated than needed, so if you'll find something simple use it. 
But at least it is representative enough. Use `org.jetbrains.kotlin.test.runners.codegen.FirLightTreeBlackBoxCodegenTestGenerated.JavaInterop.testGenericSamProjectedOut`
for checking the current totally platform-dependent implementation, and if needed - `org.jetbrains.kotlin.java.direct.JavaUsingAstLegacyBoxTestGenerated.testGenericSamProjectedOut`
for the reimplementation based on `java-direct` module, the one where the problem became obvious.

Use exceptions-based debugging - add temporarily a throwing code in the key places to understand the behavior and check gradle logs for exception messages 
and stacktraces to analyze the behavior.

The key starting places are: 
- `org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade.findClass`
- `org.jetbrains.kotlin.cli.jvm.compiler.CliVirtualFileFinder.findBinaryOrSigClass` and `fndClass`
- `VirtualFileKotlinClass`
- `KotlinBinaryClassCache`

Generate the resulting analysis document with conclusiong in the .md format in the `direct-java` module.

Ask questions or ask for instructions if this information is insufficient for the research, or if you got stuck.
