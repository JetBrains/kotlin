namespace addressbook

class Contact(
  val name : String,
  val emails : List<EmailAddress>,
  val addresses : List<PostalAddress>,
  val phonenums : List<PhoneNumber>
)

class EmailAddress(
  val user : String,
  val host : String
)

class PostalAddress(
  val streetAddress : String,
  val city : String,
  val zip : String,
  val state : USState?,
  val country : Country
) {
   assert {(state == null) xor (country == Countries["US"]) }
}

class PhoneNumber(
  val country : Country,
  val areaCode : Int,
  val number : Long
)

object Countries {
  fun get(id : CountryID) : Country = countryTable[id]

  private var table : Map<String, Country>? = null
  private val countryTable : Map<String, Country>
    get() {
      if (table == null) {
        table = HashMap()
        for (line in TextFile("countries.txt").lines(stripWhiteSpace = true)) {
          table[line] = Country(line)
        }
      }
      return table
    }
}

class Country(val name : String)