$kotlin_test_internal$.beginModule();

module.exports = function() {
    var ping = require("main").api.ping;
    var Something = require("main").api.Something;

    return {
        "pingCall": function() {
            return ping(new Something())
        },
    };
};

$kotlin_test_internal$.endModule("lib");
