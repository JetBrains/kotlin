// FILE: File.kt
package pack

public open class InetAddressImpl

// FILE: Main.kt
package a

import java.net.* // should not import java.net.InetAddressImpl because it's package local
import pack.*

class X : InetAddressImpl() // should resolve to our pack.InetAddressImpl
