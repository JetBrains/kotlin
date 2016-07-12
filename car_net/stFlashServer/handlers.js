/**
 * Created by user on 7/11/16.
 */
const fs = require("fs");
const protoBuf = require("./protobufjs");
const builder = protoBuf.loadProtoFile("./proto/carkot.proto");//todo перенести как аргумент при запуске.
const protoConstructor = builder.build("carkot");
var commandPrefix = "./data/macosx-x86_64/st-flash write";
var binFilePath = "./f.bin";

function loadBin(httpContent, response) {

    var uploadClass = protoConstructor.Upload;
    var uploadObject = uploadClass.decode(httpContent);
    fs.writeFile(binFilePath, uploadObject.data.buffer, "binary", function (error) {
        var uploadResultClass = protoConstructor.UploadResult;
        var code = 0;
        if (error) {
            code = 2;
        } else {
            code = 0;
            console.log(commandPrefix + " " + binFilePath + " " + uploadObject.base);
        }
        var resultObject = new uploadResultClass({
            "stdOut": "",
            "resultCode": code,
            "stdErr": ""
        });
        var byteBuffer = resultObject.encode();
        var byteArray = [];
        for (var i = 0; i < byteBuffer.limit; i++) {
            byteArray.push(byteBuffer.buffer[i]);
        }
        response.writeHead(200, {"Content-Type": "text/plain", "Content-length": byteArray.length});
        response.write(new Buffer(byteArray));
        response.end();
    });
}

function other(request, response) {
    console.log("other");
    response.writeHead(200, {"Content-Type": "text/plain"});
    response.end();
}


exports.loadBin = loadBin;
exports.other = other;