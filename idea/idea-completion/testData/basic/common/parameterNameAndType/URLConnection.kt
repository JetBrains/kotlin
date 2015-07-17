import java.net.URLConnection

fun foo(url<caret>){}

// EXIST_JAVA_ONLY: { lookupString: "urlConnection", itemText: "urlConnection: URLConnection", tailText: " (java.net)" }
// ABSENT: urlconnection
