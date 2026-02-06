define("lib", [], function() {
    return {
        "@get something-invalid"() {
            return "something invalid"
        },
        "some+value": 42,
        "+some+object%:": {
            foo: "%%++%%"
        }
    };
});