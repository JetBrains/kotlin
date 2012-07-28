//FILE:a.kt
//KT-1579 Can't import nested class/trait
package lib
trait WithInner {
    trait Inner {
    }
}
//FILE:b.kt
package user
import lib.WithInner.Inner