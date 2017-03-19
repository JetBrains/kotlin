package source

import target.targetPackFun

fun sourcePackFun(){}

<selection>
/* comment 1 */

fun foo() {
    sourcePackFun()
    targetPackFun()
}

/* comment 2 */

val bar = 10

/* comment 3 */
</selection>
