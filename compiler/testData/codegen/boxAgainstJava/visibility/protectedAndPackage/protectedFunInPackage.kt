package protectedPack

fun box(): String {
   return protectedFunInPackage().foo()!!
}
