/**
 * Created by user on 7/11/16.
 */
const fs = require("fs");
const main = require("./main.js");
const exec = require('child_process').exec;

function loadBin(httpContent, response) {

    console.log(httpContent.length);
    var uploadClass = main.protoConstructorCarkot.Upload;
    var uploadObject = uploadClass.decode(httpContent);
    fs.writeFile(main.binFilePath, uploadObject.data.buffer, "binary", function (error) {
        var uploadResultClass = main.protoConstructorCarkot.UploadResult;
        var code = 0;
        var stdErr = "";
        if (error) {
            code = 2;
            stdErr = error.toString();
        } else {
            code = 0;
            var shCommand = main.commandPrefix + " " + main.binFilePath + " " + uploadObject.base;
            exec(shCommand, function (error, stdout, stderr) {
                // TODO get program result code and error textual representation and send
                // back in the response
                if (error) {
                    code = 1;
                    console.error(error);
                }

                var resultObject = new uploadResultClass({
                    "stdOut": stdout.toString(),
                    "resultCode": code,
                    "stdErr": stderr.toString()
                });
                var byteBuffer = resultObject.encode();
                var byteArray = [];
                for (var i = 0; i < byteBuffer.limit; i++) {
                    byteArray.push(byteBuffer.buffer[i]);
                }
                response.writeHead(200, {"Content-Type": "text/plain", "Content-length": byteArray.length});
                response.write(new Buffer(byteArray));
                response.end();
                console.log(shCommand);
            });
        }

    });
}

function other(httpContent, response) {
    console.log("other");
    response.writeHead(200, {"Content-Type": "text/plain"});
    response.end();
}

//контроль машинкой, как с пульта управления
function control(httpContent, response) {
    var directionClass = main.protoConstructorControl.Direction;
    var directionObject = directionClass.decode(httpContent);
    var resultByte;
    switch (directionObject.command) {
        case directionClass.Command.stop :
        {
            resultByte = 0;
            break;
        }
        case directionClass.Command.forward :
        {
            resultByte = 1;
            break;
        }

        case directionClass.Command.backward :
        {
            resultByte = 2;
            break;
        }
        case directionClass.Command.right :
        {
            resultByte = 4;
            break;
        }
        case directionClass.Command.left :
        {
            resultByte = 3;
            break;
        }

    }
    console.log("byte for car: " + resultByte);
    response.writeHead(200, {"Content-Type": "text/plain"});
    response.end();
}

exports.loadBin = loadBin;
exports.other = other;
exports.control = control;
