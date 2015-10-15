// See KT-969
fun f() {
  var s: String?
  s = "a"
  var s1 = "" // String � ?
  if (<!SENSELESS_COMPARISON!>s != null<!>) {    // Redundant
    s1.length
    // We can do smartcast here and below
    s1 = <!DEBUG_INFO_SMARTCAST!>s<!>.toString() // return String?
    s1.length
    s1 = <!DEBUG_INFO_SMARTCAST!>s<!>
    s1.length
    // It's just an assignment without smartcast
    val s2 = s
    // But smartcast can be done here
    <!DEBUG_INFO_SMARTCAST!>s2<!>.length
    // And also here
    val s3 = <!DEBUG_INFO_SMARTCAST!>s<!>.toString()
    s3.length
  }
}