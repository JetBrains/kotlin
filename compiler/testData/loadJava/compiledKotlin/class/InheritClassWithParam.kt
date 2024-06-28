// TARGET_BACKEND: JVM
package test

abstract class Aaa<P>()

class Bbb() : Aaa<java.util.Random>()
