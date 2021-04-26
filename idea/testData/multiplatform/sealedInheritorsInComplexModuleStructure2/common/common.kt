package foo

/*
 * - Sealed1 [common], actualized in [main]
 *   - Sealed2 [common], actualized in [intermediate]
 *     - Derived11 [common]
 *     - Derived12 [intermediate]
 *   - Derived1 [common]
 *   - Derived2 [intermediate]
 *   - Derived3 [main]
 */
expect sealed class <!LINE_MARKER("descr='Has subclasses'")!>Sealed1<!>()
expect sealed class <!LINE_MARKER("descr='Is subclassed by Derived11 Derived12 Derived13Error'"), LINE_MARKER("descr='Has actuals in JVM'")!>Sealed2<!>() : Sealed1

class Derived1 : Sealed1()
class Derived11 : Sealed2()
