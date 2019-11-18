/*
 * See org/jetbrains/kotlin/fir/java/scopes/JavaClassUseSiteMemberScope.kt:93
 */

// FILE: CommonDataKeys.java

public class CommonDataKeys {
  public static final String PROJECT = "project";
  public final String MEMBER = "member"
}

// FILE: PlatformDataKeys.java

public class PlatformDataKeys extends CommonDataKeys {

}

// FILE: main.kt

fun test() {
    val project = PlatformDataKeys.<!UNRESOLVED_REFERENCE!>PROJECT<!>
    val member = PlatformDataKeys().MEMBER
}