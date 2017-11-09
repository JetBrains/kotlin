// FILE: 1.kt
package a
import b.ObserverSupport

interface IEntity

fun IEntity(<!UNUSED_PARAMETER!>f<!>: ObserverSupport<IEntity>) {}

// FILE: 2.kt
package b
import a.IEntity

class ObserverSupport<T : IEntity>