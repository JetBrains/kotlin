globalThis["@get something-invalid"] = function() {
    return "something invalid"
}

globalThis["some+value"] = 42

globalThis["+some+object%:"] = {
    foo: "%%++%%"
}
