 interface Base {
     fun check()
 }


 class My {
     lateinit var delegate: Base

     fun check() = delegate.check() // Should not resolve
 }