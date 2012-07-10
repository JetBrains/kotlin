fun test() {
   if (1 is Int) {
     if (1 is <!INCOMPATIBLE_TYPES!>Boolean<!>) {

     }
   }
}
