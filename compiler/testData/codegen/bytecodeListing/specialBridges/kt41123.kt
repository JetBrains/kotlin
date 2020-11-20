// WITH_RUNTIME

// TODO KT-42391
// JVM:
//    public bridge final method get(p0: java.lang.Object): java.lang.Object
//    public bridge final method remove(p0: java.lang.Object): java.lang.Object
// JVM_IR:
//    public bridge final method get(p0: java.lang.Object): java.lang.String
//    public bridge final method remove(p0: java.lang.Object): java.lang.String

open class A : HashMap<String, String>()

class B : A()