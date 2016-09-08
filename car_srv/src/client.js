/**
 * Created by user on 7/22/16.
 */
const main = require("./main.js");
const http = require("http");
function sendData(data, url, callBack) {
    var options = {
        hostname: main.serverAddress,
        port: main.serverPort,
        path: url,
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Content-Length': Buffer.byteLength(data)
        }
    };
    var req = http.request(options, function (response) {
        var answerData = [];
        response.on("data", function (datas) {
            for (var i = 0; i < datas.length; i++) {
                answerData.push(datas[i]);
            }
        });
        response.on("end", function () {
            callBack(answerData)
        });
    });
    //todo errors
    req.write(data);
    req.end();
}

exports.sendData = sendData;