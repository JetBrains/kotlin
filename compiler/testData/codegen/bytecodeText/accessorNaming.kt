package a

interface I {
    var simpleFoo: Int

    var isSomething1: Boolean
    var isSomethingNullable: Boolean?
    var isSomethingNonBoolean: String
    var isHTML: Boolean
    var is1: Boolean
    var `is`: Boolean

    var kClassName: String
    var URL: String
    var HTTPProtocol: String

    var issueFlag: Boolean

    var русскаяПропертя: Int // should not capitalize it because we only do it for ASCII
}

// 1 getSimpleFoo\(
// 1 setSimpleFoo\(
// 1 isSomething1\(
// 1 setSomething1\(
// 1 isSomethingNullable\(
// 1 setSomethingNullable\(
// 1 isSomethingNonBoolean\(
// 1 setSomethingNonBoolean\(
// 1 isHTML\(
// 1 setHTML\(
// 1 is1\(
// 1 set1\(
// 1 getIs\(
// 1 setIs\(
// 1 getKClassName\(
// 1 setKClassName\(
// 1 getURL\(
// 1 setURL\(
// 1 getHTTPProtocol\(
// 1 setHTTPProtocol\(
// 1 getIssueFlag\(
// 1 setIssueFlag\(
// 1 getрусскаяПропертя\(
// 1 setрусскаяПропертя\(
