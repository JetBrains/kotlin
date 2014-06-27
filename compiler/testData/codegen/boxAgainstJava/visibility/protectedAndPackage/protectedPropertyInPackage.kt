package protectedPack

fun box(): String {
   return protectedPropertyInPackage().foo!!
}
