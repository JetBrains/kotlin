// RUN_PIPELINE_TILL: BACKEND
// LATEST_LV_DIFFERENCE
package foo

import java.util.*

fun main()
{
  val c = ArrayList<Int>()
  c.add(3)
  System.out.println(++<!WRAPPED_LHS_IN_ASSIGNMENT_WARNING!>(c[0])<!>)
  System.out.println(<!WRAPPED_LHS_IN_ASSIGNMENT_WARNING!>(c[1])<!>--)
  System.out.println(-(c[2]))
}