// "Remove conflicting import for 'java.util.ArrayList'" "true"
package test

import java.util.ArrayList<caret>
import java.util.HashMap as ArrayList

fun foo(a : ArrayList<String, String>) {

}