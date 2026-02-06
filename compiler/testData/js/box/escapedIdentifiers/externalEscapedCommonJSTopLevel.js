$kotlin_test_internal$.beginModule("lib");
module.exports = {
    "@get something-invalid"() {
        return "something invalid"
    },
    "some+value": 42,
    "+some+object%:": {
        foo: "%%++%%"
    }
}
$kotlin_test_internal$.endModule("lib");