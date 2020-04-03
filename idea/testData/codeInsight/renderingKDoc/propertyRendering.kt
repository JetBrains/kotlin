
/**
 * Hello darling
 * @author DarkWing Duck
 */
val prop = 2

/**
 * Hello darling var
 * @author Megavolt
 */
var prop = 2


class outherClass {
    /**
     * Hello darling instance
     * @author Morgana Macawber
     */
    val instanceProp get() {
        /**
         * Hello darling local
         * @author Launchpad McQuack
         */
        val localProp get() {
            if (true) {
                /**
                 * Hello darling superLocal
                 * @author Reginald Bushroot
                 */
                val superLocalProp = 4
            }
        }
    }
}

// RENDER: <div class='content'><p>Hello darling</p></div><table class='sections'><tr><td valign='top' class='section'><p>Author:</td><td valign='top'>DarkWing Duck</td></table>
// RENDER: <div class='content'><p>Hello darling var</p></div><table class='sections'><tr><td valign='top' class='section'><p>Author:</td><td valign='top'>Megavolt</td></table>
// RENDER: <div class='content'><p>Hello darling instance</p></div><table class='sections'><tr><td valign='top' class='section'><p>Author:</td><td valign='top'>Morgana Macawber</td></table>
// RENDER: <div class='content'><p>Hello darling local</p></div><table class='sections'><tr><td valign='top' class='section'><p>Author:</td><td valign='top'>Launchpad McQuack</td></table>
// RENDER: <div class='content'><p>Hello darling superLocal</p></div><table class='sections'><tr><td valign='top' class='section'><p>Author:</td><td valign='top'>Reginald Bushroot</td></table>