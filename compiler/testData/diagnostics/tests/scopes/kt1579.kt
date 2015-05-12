//FILE:a.kt
//KT-1579 Can't import nested class/interface
package lib
interface WithInner {
    interface Inner {
    }
}
//FILE:b.kt
package user
import lib.WithInner.Inner