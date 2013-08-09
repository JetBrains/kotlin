//test for KT-3702 Inner class constructor cannot be invoked in override function with receiver
package second

public class Outer() {
    inner class Inner(test: String)
}