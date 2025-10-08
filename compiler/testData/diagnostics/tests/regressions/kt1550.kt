// LANGUAGE: +ForbidParenthesizedLhsInAssignments
// RUN_PIPELINE_TILL: FRONTEND
package foo

import java.util.*

fun main()
{
  val c = ArrayList<Int>()
  c.add(3)
  System.out.println(++(c[0]))
  System.out.println((c[1])--)
  System.out.println(-(c[2]))
}

/* GENERATED_FIR_TAGS: flexibleType, functionDeclaration, incrementDecrementExpression, integerLiteral, javaFunction,
javaProperty, localProperty, propertyDeclaration, unaryExpression */
